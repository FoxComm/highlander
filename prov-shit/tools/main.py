import argparse
import sys
import traceback
from importlib import import_module

# define tools modules
modules = [
    'caniuse',
]

# define tools dictionary, importing tools
tools = {module: import_module(module) for module in modules}

# create the top-level parser
parser = argparse.ArgumentParser(description="CLI helper tools")
subparsers = parser.add_subparsers(dest='tool')

# register tools
for name, tool in tools.iteritems():
    subparser = subparsers.add_parser(name)
    tool.register(subparser)

# parse agrs
args = parser.parse_args()

# run matched tool
try:
    tools[args.tool].handle(args)
except:
    traceback.print_exc()
    sys.exit(1)
