#!/bin/bash

#remove the files created previously
rm -r ./output ./src/*.class
#create output dir
mkdir ./output
echo "Previous files removed."
#compile and run java
echo "compile and run java:"
javac ./src/*.java
cd ./src
java H1B_TopK ../input/h1b_input.csv ../output/top_10_occupations.txt ../output/top_10_states.txt
