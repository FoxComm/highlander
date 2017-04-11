def color(color, text):
    return color + text + '\033[0m'


def red(text):
    return color('\033[31m', text)


def green(text):
    return color('\033[32m', text)


def yellow(text):
    return color('\033[33m', text)


def blue(text):
    return color('\033[34m', text)
