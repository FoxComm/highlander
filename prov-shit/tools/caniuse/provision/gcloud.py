from caniuse.common import Test


def check():
    def nix_test(run):
        out = run("apt-cache policy {package}", package="google-cloud-sdk")

        return out.find("Installed: (none)") < 0

    def nix_help(run):
        out = run("apt-cache policy {package}", package="google-cloud-sdk")

        # if no package
        if not out:
            print (
                "Add google-cloud-sdk ppa repository:\n"
                "$ export GOOGLE_CLOUD_SDK_REPO=\"cloud-sdk-$(lsb_release -cs)\"\n"
                "$ echo \"deb http://packages.cloud.google.com/apt $GOOGLE_CLOUD_SDK_REPO main\" | sudo tee /etc/apt/sources.list.d/google-cloud-sdk.list\n"
                "$ curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -\n"
                "$ sudo apt-get update\n"
            )

        print (
            "Install gcloud:\n"
            "$ sudo apt-get install google-cloud-sdk\n"
            "Init gclod:\n"
            "$ gcloud init\n"
        )

    return Test(
            "Is gcloud installed?",
            nix=(nix_test, nix_help)
    )
