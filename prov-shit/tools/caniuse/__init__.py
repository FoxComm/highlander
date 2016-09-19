from importlib import import_module

projects = [
    'ashes',
    'firebrand',
    'provision',
]


def register(parser):
    parser.add_argument("project")


def handle(args):
    if args.project not in projects:
        raise Exception("Unknown project")

    import_module(__name__ + '.' + args.project).caniuse()
