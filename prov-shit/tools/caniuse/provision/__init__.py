import ansible
import pip
from caniuse.common import run_tests


def caniuse():
    run_tests("provisioning", [
        pip.check(),
        ansible.check(),
    ])
