from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("apt-cache policy {package}", package="libssl-dev")

        return out.find("Installed: (none)") < 0

    def nix_help(run):
        print (
            "Install libssl-dev:\n"
            "$ sudo apt-get install libssl-dev\n"
        )

    return Test(
            "Is libssl-dev installed?",
            nix=(nix_test, nix_help)
    )
