from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("apt-cache policy {package}", package="vagrant")

        return out.find("Installed: 1:1.8.") >= 0

    def nix_help(run):
        print (
            "Install vagrant:\n"
            "Open https://www.vagrantup.com/downloads.html\n"
            "Copy link to package for your distro"
            "$ wget <link>\n"
            "$ sudo dpkg -i <deb file>"
            "$ rm <deb file>"
        )

    return Test(
            "Is vagrant installed?",
            nix=(nix_test, nix_help)
    )
