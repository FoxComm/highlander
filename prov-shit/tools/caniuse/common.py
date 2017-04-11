import platform
import shlex
from subprocess import Popen, PIPE

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
        if not system in test.tests:
            all_passed = False
            print yes_or_no(test.desc, False)
            continue

        tester, help = test.tests[system]
        result = tester(run)
        print yes_or_no(test.desc, result)
        if result:
            continue

        all_passed = False
        if callable(help):
            help(run)
        else:
            print help

    # print result
    if all_passed:
        print green("Congrats! You can run {project}".format(project=project))
    else:
        print red("Sorry... You can't run {project}".format(project=project))


class Test():
    def __init__(self, desc, **kwargs):
        self.desc = desc
        self.tests = kwargs


def run(cmdTemplate, **kwargs):
    cmd = cmdTemplate.format(**kwargs)
    args = shlex.split(cmd)
    process = Popen(args, stdin=PIPE, stdout=PIPE, stderr=PIPE)

    return process.communicate()[0]


def yes_or_no(description, value):
    result = "-> " + description + ": "
    if value:
        return result + green("yes")

    return result + red("no")
