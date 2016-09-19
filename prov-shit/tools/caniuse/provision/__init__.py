import ansible
import pip
import ansible_lint
from caniuse.common import run_tests


def caniuse():
    run_tests("provisioning", [
        pip.check(),
        ansible.check(),
        ansible_lint.check(),
    ])
