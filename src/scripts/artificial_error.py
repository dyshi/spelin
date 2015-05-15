import bmprocess
import random
import csv
import re

#file used to inject errors into correct words
#paper by Whitelaw et al (2011) suggest that artificial pairs are
#suitable for use to train the speller's confidence classfier for candidates

#probability of producing an error
ERROR_PROB = 1.0
#threshold for giving up if we can't find a suitable transform
THRESHOLD = 100
#get the transform map
flattened_map = bmprocess.get_flattened_map()

def markup_line(line):
  return '^' + line.replace(" ", "$ ^") + '$'

def clean_line(line):
  return line.replace(".","").replace("$", "").replace("^", "")

#We want the non regex version sometimes
def cleaned_pattern(pattern):
  return pattern.replace(".", "").replace("[a-zA-Z]", "").replace("\$", "").replace("\^", "").replace("$","").replace("^","")
#we need to markup the the pattern 
def markup_pattern(pattern):
  return pattern.replace("$", "\$").replace("^", "\^").replace(". .", "\$ \^").replace(".","[a-zA-Z]")

#method will randomly find a position and make the error
def simulate_error(original, pattern, replacement):
  original = markup_line(original)
  #NOTE since we markedup the original with these symbols, we have to also escape those in the pattern
  pattern = markup_pattern(pattern)
  #find all the places where this matches
  pieces = re.findall(pattern, original)
  #randomly pick a spot
  #stripped out the regex
  piece_to_replace = random.sample(pieces, 1)[0]
  #now we want to take the middle piece in the context and replace it with the pattern
  new_piece = piece_to_replace[0] + replacement[1:-1] + piece_to_replace[-1]
  #However, sometimes the piece to replace is very small, and there are multiple substrings of it
  #we need to account for this to be more specific and pick only one instance
  #so we need to split the original
  #debug print original, pattern, replacement, pieces, piece_to_replace, new_piece

  shattered = original.split(piece_to_replace)
  pattern_location = random.randint(1, len(shattered)-1)
  split_pieces = [piece_to_replace.join(shattered[:pattern_location]), piece_to_replace.join(shattered[pattern_location:])]

  #because sometimes the first piece is joined is empty sometimes
  #so we want to check, that we will not be putting the new_piece into the front,
  #except that it is supposed to be
  if split_pieces[0] == '' and not new_piece[0] == '^':
    new_line= new_piece.join(split_pieces[1:])
  else :
    new_line = new_piece.join(split_pieces)
  return clean_line(new_line)

def assign_num_errors(line):
  num_words_in_line = len(line.split(" "))
  return 1
  #return min(3,int(num_words_in_line / (2.0 * random.random())))

#here we want to add some probability, very small for beginning of word
#return boolean
BEGINNING_WORD_THRESHOLD = 0.02
def pattern_in_line(pattern, line):
  value = random.random()
  return not pattern == '' and len(re.findall(markup_pattern(pattern), markup_line(line))) > 0

#TODO here we can simulate unseen transformations, or we
#can simulate it further down
transformations = []
for key in flattened_map.keys():
  for i in range(flattened_map[key]):
    transformations.append(key)

random.shuffle(transformations)
#first open the file to insert errors
inputFile = open("cleanedNameQueryData.csv")
outputFile = csv.writer(open("artificialErrors3.csv", "wb"))
for line in inputFile.readlines():
  random_event = random.random()
  line = line.strip()
  original = line
  transformation_used = () 
  if (random_event <= ERROR_PROB):
    unchanged = True
    count = 0
    #we use this variable to inject multiple errors
    num_errors = assign_num_errors(line)
    error_count = 0
    while (unchanged or error_count < num_errors) and count < THRESHOLD:
      count += 1
      transformation  = random.sample(transformations, 1)
      first, second = transformation[0]

      #we have to also make sure that we don't usually replace at the beginning of word
      if pattern_in_line(first, line):
        line = simulate_error(line, first, second)
        unchanged = False
        error_count +=1
        transformation_used = transformation
      elif pattern_in_line(second, line):
        line = simulate_error(line, second, first)
        unchanged = False
        error_count += 1
        transformation_used = transformation
    if unchanged:
      print "error"
  outputFile.writerow([original, line, transformation_used])
