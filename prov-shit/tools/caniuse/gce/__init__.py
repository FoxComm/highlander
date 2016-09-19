import env_google_ssh_username
import vagrant_google_box
import vagrant_plugin_google
from caniuse.common import run_tests
from caniuse.gce import vagrant_plugin_google


def caniuse():
    run_tests("GCE", [
        vagrant_plugin_google.check(),
        vagrant_google_box.check(),
        env_google_ssh_username.check(),
    ])
