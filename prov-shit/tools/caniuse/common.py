from lib.output import *


def yesOrNo(value):
    if value:
        return green("yes")

    return red("no")
