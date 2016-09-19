from common import Test, run_tests


def caniuse():
    run_tests("provisioning", [
        check_ansible(),
    ])


def check_ansible():
    def nix_run(run):
        out = run("apt-cache policy {package}", package="ansible")

        return out.find("Installed: 1.9.") >= 0

    def nix_help(run):
        out = run("apt-cache policy {package}", package="ansible")

        # if candidate not available
        if out.find("1.9.") < 0:
            print (
                "Add ansible-1.9 ppa repository:\n"
                "$ sudo apt-add-repository ppa:ansible/ansible-1.9\n"
                "$ sudo apt-get update\n"
            )
        print (
            "Install ansible:\n"
            "$ sudo apt-get install ansible=1.9.x\n"
            "instead <x> use proper available version\n"
        )

    return Test(
            "Is ansible 1.9.x installed?",
            nix=(nix_run, nix_help)
    )
