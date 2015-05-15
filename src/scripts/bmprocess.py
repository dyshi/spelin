import sys
import re
import string
import csv

#implemented according to Brill and Moore (2000) 
#to extract error patterns in spelling correction pairs
#pairs then used in the speller's ranking models

#To use, call get_count_map for a map of (pattern, replacement) -> count
#window size used to compute the transformation pairs
#Window size, 4 was the optimal size it seemed
WINDOW_SIZE = 4 

regex = re.compile("[%s]" % re.escape(string.punctuation))
def normalize_token(token):
  return regex.sub('', token.strip().lower()) 

#first find the edit distance
def align_words(original, candidate):
  #dp implementation of edit distance

  #cost matrix size candidate by original
  cost = [range(0, len(original) + 1)]

  #first do DP on getting a cost 
  for i in range(len(candidate)):
    cost.append([i+1 for j in range(0,len(original)+1)])

  for i in range(len(candidate)):
    for j in range(len(original)):
      if (candidate[i] == original[j]):
        cost[i+1][j+1] = cost[i][j]
      #transposition
      elif (i > 0 and j > 0 and candidate[i-1] == original[j] and candidate[i] == original[j-1]):
        cost[i+1][j+1] = cost[i-1][j-1] + 1
      elif (not candidate[i] == original[j]):
        cost[i+1][j+1] = min(cost[i][j], min(cost[i][j+1], cost[i+1][j])) + 1  


  #Debug prints
  #print "Cost: "
  #print cost[len(candidate)][len(original)]

  #now backtrack and align the words accordingly
  current_cost = cost[len(candidate)][len(original)]
  candidate_index = len(candidate)
  original_index = len(original)
  original_array = []
  candidate_array = []
  #print cost
  while (candidate_index > 0 and original_index > 0):
    #transposition error
    if (original_index > 1 and candidate_index > 1 and candidate[candidate_index-2] == original[original_index-1] and candidate[candidate_index-1] == original[original_index-2] and not candidate[candidate_index-1] == original[original_index-1]):
      #print "transposition"
      candidate_array.append(candidate[candidate_index-1])
      candidate_array.append(candidate[candidate_index-2])
      original_array.append(original[original_index-1])
      original_array.append(original[original_index-2])
      original_index -=2
      candidate_index -=2
      current_cost -=1
    #deletion error
    elif ( (not original[original_index-1] == candidate[candidate_index-1]) and cost[candidate_index-1][original_index] == current_cost -1):
      #print "deletion"
      candidate_index -=1
      current_cost-=1
      original_array.append("")
      candidate_array.append(candidate[candidate_index])
    #insertion error
    elif ( (not original[original_index-1] == candidate[candidate_index-1]) and cost[candidate_index][original_index-1] == current_cost -1):
      #print "insertion"
      original_index -= 1
      current_cost -=1
      original_array.append(original[original_index])
      candidate_array.append("")
    #match
    elif (cost[candidate_index-1][original_index-1] == current_cost and original[original_index -1] == candidate[candidate_index-1]):
      #print "match"
      original_index -=1 
      candidate_index -=1
      original_array.append(original[original_index])
      candidate_array.append(candidate[candidate_index])
    #transformation
    elif (cost[candidate_index-1][original_index-1] == current_cost -1 and (not original[original_index-1] == candidate[candidate_index-1])):
      #print "transform"
      original_index -=1
      candidate_index-=1
      original_array.append(original[original_index])
      candidate_array.append(candidate[candidate_index])
      current_cost-=1
    else :
      #Should never happen
      None
      #print candidate_index, original_index
      #print current_cost, cost[candidate_index-1][original_index-1]
      #print candidate[candidate_index-1], original[original_index-1]

  #there may be left over in that one is 0, but we haven't push the other yet
  while (candidate_index > 0):
    candidate_array.append(candidate[candidate_index-1])
    original_array.append("")
    candidate_index -= 1
  while (original_index > 0):
    original_array.append(original[original_index - 1])
    candidate_array.append("")
    original_index -=1
  #return aligned arrays, and the cost
  return [original_array[::-1], candidate_array[::-1], cost[len(candidate)][len(original)]]

#just a visual test
def test_align_words():
  #test cases for insertion deletion, empty
  #as well as matching
  print align_words("match", "match")
  print align_words("testing", "tsting")
  print align_words("abc","b")
  print align_words("tets", "testing")
  print align_words("", "abc")
  print align_words("transposition", "transpositoin")
  print align_words("delete", "deleted")
  print align_words("trans", "trasn")

#NOTE taking indices as the patterns are constructed, ei [0: n+1]
#add the regex placeholders to denote beginning, end, or middle of a word
def add_context(begin_index, end_index, pattern, line_length):
  #special case
  if pattern == '':
    return pattern

  if (begin_index == 0):
    pattern = '^' + pattern
  else:
    pattern = '.' + pattern

  if (end_index == line_length):
    pattern = pattern + '$'
  else :
    pattern = pattern + '.'
  return pattern

#compute the transform pairs
def bm_pairings(original, candidate):
  aligned_original, aligned_candidate, cost = align_words(original, candidate)
  #will iterate until we find mismatches, and then push the mismatches into the list
  string_pairs = []
  length = len(aligned_original)
  for i in range(length):
    if (True or not aligned_original[i] == aligned_candidate[i]):
      back_index = i
      forward_index = i
      while (back_index >= 0 and back_index >= i - WINDOW_SIZE):
        
        pattern = ''.join(aligned_original[back_index:i+1])
        replacement = ''.join(aligned_candidate[back_index:i+1])

        string_pairs.append(( add_context(back_index, i+1, pattern, length),add_context(back_index, i+1, replacement, length) )) 
        back_index -= 1
      #while (forward_index < len(aligned_original) and i + WINDOW_SIZE > forward_index):

      #  pattern = ''.join(aligned_original[i:forward_index + 1])
      #  replacement =''.join(aligned_candidate[i:forward_index +1]) 

      #  string_pairs.append( (add_context(i, forward_index + 1, pattern, length), add_context(i ,forward_index + 1, replacement, length)))
      #  forward_index += 1

  return [string_pairs, cost]
        
#quick visual test
def test_bm_pairings():
  print bm_pairings("actual", "akgsual")
  print bm_pairings("", "abc")
  print bm_pairings("abc", "")
  print bm_pairings("testing", "tsting")
  print bm_pairings("trans", "trasn")
  print bm_pairings("delete", "deleted")
  print bm_pairings("john", "jon")
  print bm_pairings("carring", "carrying")

#this method will take equivalent pairs ei a->b, b->a
#and consolidate
def flatten_pair_map(pair_map):
  flattened_map = {}
  for key in pair_map.keys():
    alternate = (key[1], key[0])
    if not flattened_map.has_key(key):
      value = pair_map[key]
      if pair_map.has_key(alternate):
        value += pair_map[alternate]
      flattened_map[key] = value

  return flattened_map

#iterate over the training list and construct the counts
def construct_counts_for_pairs(file_input):
  text_file = open(file_input)
  lines = text_file.readlines()
  pair_map = {}

  originals = []
  candidates = []
  for line in lines:
    if line.strip() == "\"\"":
      continue
    pieces = line.strip().split(",")
    if len(pieces) > 3 or len(pieces) < 2:
      continue
    original = pieces[0]
    candidate = pieces[1]

    originals.append(original)
    candidates.append(candidate)

    freq = 1
    try:
      recorded_freq = int(pieces[2])
    except :
      freq = 1
      
    bm_pairs,cost = bm_pairings(normalize_token(original), normalize_token(candidate))
    #filter out high cost, data not clean, so >3 likely bad data sanitation
    if cost > 3:
      continue
    for pair in bm_pairs:
      if pair_map.has_key(pair):
        pair_map[pair] += freq
      else:
        pair_map[pair] = freq

  #for each transform, we want to find the count of where it could have happened
  #a map of transform opportunity to actual replacement
  potential_transform_map = {}

  #we want to count all occurrences of the potential pattern in the original file
  #so we find all possible occurrences of the first piece in all the originals
  
  total_frequency_count = sum(pair_map[key] for key in pair_map.keys())
  total_unique_count = len(pair_map.keys())
  for key in pair_map.keys():
    if (potential_transform_map.has_key(key[0])):
      potential_transform_map[key[0]] += pair_map[key]
    else :
      potential_transform_map[key[0]] = pair_map[key]

  #we can now calculate the probability of each replacement wrt to a pattern
  for key in pair_map.keys():
    if potential_transform_map.has_key(key[0]):
      pair_map[key] = pair_map[key] / float(potential_transform_map[key[0]])
    else:
      print "error for", key

  print total_frequency_count, total_unique_count
  return pair_map

def get_flattened_map():
  flattened_map = construct_counts_for_pairs("cleanedPairs.txt")
  return flattened_map

def write_to_csv(count_map, file_name):
  writer = csv.writer(file(file_name, "wb"))
  for key in count_map.keys():
    writer.writerow([key[0], key[1], count_map[key]])
write_to_csv(get_flattened_map(), "bm_pairs.csv")
#This was used to see the frequency of transforms
#flattened_map = get_flattened_map()
#for key in flattened_map.keys():
#  if count_map.has_key(flattened_map[key]):
#    count_map[flattened_map[key]] += 1
#  else :
#    count_map[flattened_map[key]] =1
#for key in count_map.keys():
#  print key, count_map[key]
