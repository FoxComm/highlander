from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("apt-cache policy {package}", package="golang")

        return out.find("Installed: 2:1.6-") >= 0 \
               or out.find("Installed: 2:1.7-") >= 0 \
               or out.find("Installed: 2:1.8-") >= 0

    def nix_help(run):
        print (
            "Install Go:\n"
            "$ sudo apt-get install golang=1.x\n"
            "instead <x> use proper available version\n"
        )

    return Test(
            "Is Go 1.6+ installed?",
            nix=(nix_test, nix_help)
    )
