#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os, sys
import os.path
import re
import json
import subprocess as sp
import urllib.request as request
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("-docker", help="Build docker containers also", action="store_true")
parser.add_argument("-debug", help="Don't take any action", action="store_true")
global_args = parser.parse_args()

ROOT_DIR=os.path.abspath(os.path.dirname(os.path.basename(__file__)))

PROJECTS = (
    'ashes',
    'data-import',
    'demo/peacock',
    'developer-portal',
    'green-river',
    'geronimo',
    'hyperion',
    'intelligence/anthill',
    'intelligence/bernardo',
    'intelligence/consumers/digger-sphex',
    'intelligence/consumers/orders-anthill',
    'intelligence/consumers/orders-reviews',
    'intelligence/consumers/orders-sphex',
    'intelligence/consumers/product-activity',
    'intelligence/eggcrate',
    'intelligence/river-rock',
    'intelligence/suggester',
    'intelligence/user-simulation',
    'isaac',
    'messaging',
    'middlewarehouse',
    'middlewarehouse/common/db/seeds',
    'middlewarehouse/consumers/capture',
    'middlewarehouse/consumers/customer-groups',
    'middlewarehouse/consumers/gift-cards',
    'middlewarehouse/consumers/shipments',
    'middlewarehouse/consumers/shipstation',
    'middlewarehouse/elasticmanager',
    'onboarding/service',
    'onboarding/ui',
    'phoenix-scala',
    'phoenix-scala/seeder',
    'solomon',
    'tabernacle/docker/neo4j',
    'tabernacle/docker/neo4j_reset',
)

for p in PROJECTS:
    d = os.path.join(ROOT_DIR, p)
    assert os.path.exists(d), "Project directory {} doesn't exist".format(p)

def str_env(msg):
    subst = re.sub(r"\$(?P<param>\w+)", r"{\g<1>}", msg)
    return subst.format(**os.environ)

def log(*args):
    new_args = map(str_env, args)
    print("[BUILDER]", *new_args)

def get_base_branch():
    if os.environ.get("BUILDKITE_PULL_REQUEST") != "false":
        github_base_url = "https://api.github.com/repos/FoxComm/highlander/pulls"
        log("Fetching base branch for PR#$BUILDKITE_PULL_REQUEST via Github API...")
        url = github_base_url + str_env("/$BUILDKITE_PULL_REQUEST?access_token=$GITHUB_API_TOKEN")
        with request.urlopen(url) as resp:
            answer = json.loads(resp.read().decode('utf-8'))
            return "origin/" + answer['base']['ref']
    else:
        log("No pull request created, setting base branch to master")
        return "master"

def all_changed_dirs(base_branch):
    commit = os.environ['BUILDKITE_COMMIT']
    args = ["git", "diff", "--name-only", "{base_branch}...{commit}".format(**locals())]
    result = sp.run(args, stdout=sp.PIPE, check=True)
    stdout = result.stdout.decode('UTF-8')
    return {os.path.dirname(x) for x in stdout.split("\n") if x}

def changed_projects(changed_dirs):
    projects = set()
    for d in changed_dirs:
        all_affected = [p for p in PROJECTS if d.startswith(p)]
        if all_affected:
            # return project with max overlap in dirs
            detected = max(all_affected, key=lambda v: len(v))
            projects.add(detected)
    return projects

def run_process(args, working_dir):
    child = sp.Popen(args, cwd=working_dir)
    child.wait()
    if child.returncode != 0:
        sys.exit(child.returncode)

def main():
    base_branch = get_base_branch()
    changed_dirs = all_changed_dirs(base_branch)
    affected_projects = changed_projects(changed_dirs)

    log("Changed projects ({}):".format(len(affected_projects)))
    for p in affected_projects:
        log("\t", p)

    if len(affected_projects) == 0:
        if os.environ.get("BUILDKITE_PIPELINE_SLUG") == "highlander-master":
            log("Rebuilding everything")
            affected_projects = PROJECTS
        else:
            log("No projects changed, nothing to build")
            sys.exit(0)

    # don't do anything else
    if global_args.debug:
        sys.exit(0)

    sp.run("git fetch origin -q".split(" "), check=True)

    log("Building subprojects...")
    for project_dir in affected_projects:
        absdir = os.path.join(ROOT_DIR, project_dir)
        run_process(["make", "build", "test"], absdir)
        if global_args.docker:
            run_process(["make", "docker", "docker-push"], absdir)

if __name__ == "__main__":
    main()
