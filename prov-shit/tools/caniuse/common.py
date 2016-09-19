import platform

from lib.output import *


def get_system():
    sys = platform.system()
    if sys == "Linux" or sys == "Linux2":
        return "nix"

    if sys == "Darwin":
        return "osx"

    if sys == "Windows":
        return "win"

    raise Exception("Don't know platform {sys}".format(sys=sys))


system = get_system()


def run_tests(project, tests):
    all_passed = True

    # run tests
    for test in tests:
        result = test.tester(run)
        print yes_or_no(test.desc, result)
        if not result:
            all_passed = False
            if system in test.help:
                help = test.help[system]
                if callable(help):
                    print help(run)
                else:
                    print help

    # print result
    if all_passed:
        print green("Congrats! You can run {project}".format(project=project))
    else:
        print red("Sorry... You can't run {project}".format(project=project))


class Test():
    def __init__(self, desc, tester, **kwargs):
        self.desc = desc
        self.tester = tester
        self.help = kwargs


def run(cmd):
    print "running {cmd}".format(cmd=cmd)


def yes_or_no(description, value):
    result = "-> " + description + ": "
    if value:
        return result + green("yes")

    return result + red("no")
