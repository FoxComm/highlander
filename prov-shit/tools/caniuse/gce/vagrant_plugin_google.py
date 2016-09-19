from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("vagrant plugin list")

        return out.find("vagrant-google") >= 0

    def nix_help(run):
        print (
            "Install vagrant-google plugin for vagrant:\n"
            "$ vagrant plugin install vagrant-google\n"
        )

    return Test(
            "Is vagrant plugin vagrant-google installed?",
            nix=(nix_test, nix_help)
    )
