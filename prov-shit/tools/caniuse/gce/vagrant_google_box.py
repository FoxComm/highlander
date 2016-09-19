from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("vagrant box list")

        return out.find("gce") >= 0

    def nix_help(run):
        print (
            "Add vagrant google box for vagrant:\n"
            "$ vagrant box add gce https://github.com/mitchellh/vagrant-google/raw/master/google.box\n"
        )

    return Test(
            "Is vagrant google box added?",
            nix=(nix_test, nix_help)
    )
