import ansible
from caniuse.common import run_tests


def caniuse():
    run_tests("provisioning", [
        ansible.check_ansible(),
    ])
