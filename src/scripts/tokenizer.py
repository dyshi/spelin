import sys
import re
import string

regex = re.compile("[%s]" % re.escape(string.punctuation))
def normalizeToken(token):
  return regex.sub('', token.strip().lower()) 

def construct_unigram_dictionary():
  text_file= open("big.txt")
  lines = text_file.readlines()
  other_file = open("spellerData.txt")
  lines +=  other_file.readline()

  tokenMap = {}
  for line in lines:
    for token in re.split("\W", line.strip()):
      if len(token) > 2:
        token = normalizeToken(token)
        if tokenMap.has_key(token):
          tokenMap[token] += 1
        else:
          tokenMap[token] = 1


  #now print the map
  outputFile = open("dictionary.txt", "wrb")
  for key in tokenMap.keys():
    outputFile.write(key)
    outputFile.write(',')
    outputFile.write(str(tokenMap[key]))
    outputFile.write('\n')

def construct_bigram_dictionary():
  token_map = {}
  textFile = open("big.txt")
  lines = textFile.readlines()
  otherFile = open("spellerData.txt")
  lines += otherFile.readline()

  for line in lines:
    tokens = re.split("\W", line.strip());
    for i in range(len(tokens)-1):
      key = normalizeToken(tokens[i] + " " + tokens[i+1])
      if (token_map.has_key(key)):
        token_map[key] += 1
      else :
        token_map[key] =1

  outputFile = open("bigram.txt", "wrb")
  for key in token_map.keys():
    outputFile.write(key)
    outputFile.write(',')
    outputFile.write(str(token_map[key]))
    outputFile.write('\n')

#construct_unigram_dictionary()
#construct_bigram_dictionary()
