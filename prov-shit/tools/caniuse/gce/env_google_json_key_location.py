from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("printenv")

        return out.find("GOOGLE_JSON_KEY_LOCATION") >= 0

    def nix_help(run):
        print (
            "Add GOOGLE_JSON_KEY_LOCATION:\n"
            "$ echo \"export GOOGLE_JSON_KEY_LOCATION=<path to foxcomm-staging.json>\" >> ~/.bash_profile\n"
            "$ . ~/.bash_profile\n"
        )

    return Test(
            "Is GOOGLE_JSON_KEY_LOCATION env variable added?",
            nix=(nix_test, nix_help)
    )
