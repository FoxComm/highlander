import ansible
import ansible_lint
import build_essential
import libssl_dev
import pip
from caniuse.common import run_tests


def caniuse():
    run_tests("provisioning", [
        build_essential.check(),
        libssl_dev.check(),
        pip.check(),
        ansible.check(),
        ansible_lint.check(),
    ])
