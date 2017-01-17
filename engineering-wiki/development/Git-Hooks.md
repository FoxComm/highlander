# Git hooks

During development, you might also want to use our Git hooks:

    $ cd highlander/
    $ cd .git ; rm -r hooks ; ln -s ../git-hooks hooks

Currently, there’s only one that by default adds `[skip ci]` to preformatted commit messages. This means it will only be added if you edit your commit messages with an external editor, `git commit -m <msg>` won’t be affected.  If you’re finishing your work on some branch and want to build it, simply remove the line.
