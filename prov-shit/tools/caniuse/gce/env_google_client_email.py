from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("printenv")

        return out.find("GOOGLE_CLIENT_EMAIL") >= 0

    def nix_help(run):
        print (
            "Add GOOGLE_CLIENT_EMAIL:\n"
            "$ echo \"export GOOGLE_CLIENT_EMAIL=<your FoxCommerce email>\" >> ~/.bash_profile\n"
            "$ . ~/.bash_profile\n"
        )

    return Test(
            "Is GOOGLE_CLIENT_EMAIL env variable added?",
            nix=(nix_test, nix_help)
    )
