from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("apt-cache policy {package}", package="python-pip")

        return out.find("Installed: 8.") >= 0

    def nix_help(run):
        out = run("apt-cache policy {package}", package="python-pip")

        print (
            "Install python-pip:\n"
            "$ sudo apt-get install python-pip=8.x\n"
            "instead <x> use proper available version\n"
        )

    return Test(
            "Is pip 8.x installed?",
            nix=(nix_test, nix_help)
    )
