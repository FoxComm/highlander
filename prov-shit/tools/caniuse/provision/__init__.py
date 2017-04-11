import ansible
import ansible_lint
import build_essential
import gcloud
import golang
import libssl_dev
import pip
import vagrant
from caniuse.common import run_tests


def caniuse():
    run_tests("provisioning", [
        build_essential.check(),
        libssl_dev.check(),
        pip.check(),
        ansible.check(),
        ansible_lint.check(),
        golang.check(),
        gcloud.check(),
        vagrant.check(),
    ])
