from __future__ import (absolute_import, division, print_function)

__all__ = ['ActionModule']

# use this plugin for ansible 1 and 2.x https://github.com/saygoweb/ansible-plugin-copyv
# PR in upstream https://github.com/ansible/ansible/pull/14079, please vote with :thumbsup:

import ansible

import os
import sys
cur_dir = os.path.dirname(os.path.abspath(__file__))
os.sys.path.insert(1, os.path.join(cur_dir, 'copyv_lib'))

if (ansible.__version__[0] == '1'):
	from copyv1 import ActionModule
elif (ansible.__version__[0] == '2'):
	from copyv2 import ActionModule
else:
	raise RuntimeException("Can't find suitable module for ansible v %s" % ansible.__version__)
