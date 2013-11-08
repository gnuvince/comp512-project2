import sys
import csv

def average(xs):
    return sum(xs)/len(xs)

print "file,latency"
for filename in sys.argv[1:]:
    with open(filename) as f:
	c = csv.reader(f)
	c.next()
	print "%s,%f" % (filename, average([float(row[2]) for row in c]))
        
