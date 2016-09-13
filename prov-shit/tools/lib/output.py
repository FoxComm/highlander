def color(color, text):
    return color + text + '\033[0m'


def red(text):
    return color('\033[91m', text)


def green(text):
    return color('\033[92m', text)


def yellow(text):
    return color('\033[93m', text)


def blue(text):
    return color('\033[94m', text)
