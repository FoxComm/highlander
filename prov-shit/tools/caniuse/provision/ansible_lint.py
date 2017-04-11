from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("pip list")

        return out.find("ansible-lint (2.2.") >= 0

    def nix_help(run):
        out = run("pip list")

        # if version mismatch
        if out.find("ansible-lint") >= 0:
            print (
                "Check version of ansible-lint installed. It is not 2.2.x\n"
            )

        print (
            "Install ansible-lint:\n"
            "$ pip install ansible-lint==2.2.x\n"
            "instead <x> use proper available version\n"
        )

    return Test(
            "Is ansible-lint 2.2.x installed?",
            nix=(nix_test, nix_help)
    )
