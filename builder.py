#!/usr/bin/env python3

import os, sys
import os.path
import re
import json
import subprocess as sp
import urllib.request as request
import argparse


parser = argparse.ArgumentParser()
parser.add_argument("--docker", help="Build docker containers also", action="store_true")
parser.add_argument("--debug", help="Don't take any action", action="store_true")
global_args = parser.parse_args()

ROOT_DIR=os.path.abspath(os.path.dirname(os.path.basename(__file__)))

PROJECTS = (
    'ashes',
    'data-import',
    'demo/peacock',
    'developer-portal',
    'green-river',
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
    'middlewarehouse/consumers/stock-items',
    'middlewarehouse/elasticmanager',
    'onboarding',
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


def log(msg):
	print(str_env(msg))

def get_base_branch():
	if os.environ.get("BUILDKITE_PULL_REQUEST") != "false":
		github_base_url = "https://api.github.com/repos/FoxComm/highlander/pulls"
		log("Fetching base branch for PR#$BUILDKITE_PULL_REQUEST via Github API...")
		url = github_base_url + str_env("/$BUILDKITE_PULL_REQUEST?access_token=$GITHUB_API_TOKEN")
		with request.urlopen(url) as resp:
			answer = json.loads(resp.read())
			return "origin/" + answer['base']['ref']
	else:
		log("No pull request created, setting base branch to master")
		return "master"

def all_changed_dirs(base_branch):
	commit = os.environ['BUILDKITE_COMMIT']
	args = ["git", "diff", "--name-only", "{base_branch}...{commit}".format(**locals())]
	result = sp.run(args, stdout=sp.PIPE, check=True, encoding="UTF-8")
	return {os.path.dirname(x) for x in result.stdout.split("\n") if x}


def changed_projects(dirs):
	return [project for project in PROJECTS if 
					[True for d in dirs if d.startswith(project)]]

if __name__ == "__main__":
	base_branch = get_base_branch()
	changed_dirs = all_changed_dirs(base_branch)
	affected_projects = changed_projects(changed_dirs)
	if len(affected_projects) == 0:
		print("No projects changed, building all by default")
		affected_projects = PROJECTS

	print("Changed projects ({}):".format(len(affected_projects)))
	for p in affected_projects:
		print("\t", p)

	sp.run("git fetch origin -q".split(" "), check=True)

	if not global_args.debug:
		print("Building subprojects...")
		for project_dir in affected_projects:
			absdir = os.path.join(ROOT_DIR, project_dir)
			sp.Popen(["make", "build", "test"], cwd=absdir).wait()
			if global_args.docker:
				sp.Popen(["make", "docker", "docker-push"], cwd=absdir).wait()




