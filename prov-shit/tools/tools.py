import argparse
#from lib.output import *

# create the top-level parser
parser = argparse.ArgumentParser(description="CLI helper tools")

subparsers = parser.add_subparsers(dest="subparser_name")

# create the parser for "caniuse" command
caniuse_parser = subparsers.add_parser("caniuse")
caniuse_parser.add_argument("project")

args = parser.parse_args()

print args
