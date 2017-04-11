from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("apt-cache policy {package}", package="build-essential")

        return out.find("Installed: (none)") < 0

    def nix_help(run):
        print (
            "Install build-essential:\n"
            "$ sudo apt-get install build-essential\n"
        )

    return Test(
            "Is build-essential installed?",
            nix=(nix_test, nix_help)
    )
