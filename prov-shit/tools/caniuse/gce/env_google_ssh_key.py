from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("printenv")

        return out.find("GOOGLE_SSH_KEY") >= 0

    def nix_help(run):
        print (
            "Add GOOGLE_SSH_KEY:\n"
            "$ echo \"export GOOGLE_SSH_KEY=<path to your key>\" >> ~/.bash_profile\n"
            "$ . ~/.bash_profile\n"
        )

    return Test(
            "Is GOOGLE_SSH_KEY env variable added?",
            nix=(nix_test, nix_help)
    )
