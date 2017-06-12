-----

# 07 June 2017: scalafmt update
Scalafmt has been updated to work as a standalone binary rather than SBT plugin.
Run `utils/scalafmt/update-scalafmt.sh` to read more about new process and have everything set up!
From now on, `git config core.hooksPath` must be set to `$HIGHLANDER/utils/git-hooks` (this configuration is covered by the above script).
If you hit any issues, please report immediately!

_Happy hacking!
-- Anna_

-----

# 27 May 2017: new project structure, changes in imports, style guide
Hey, we now have a changelog! W00t!

## New project structure

### Project tree
Now that we're moving towards splitting phoenix in submodules, our new project structure is
```
root (phoenix-scala dir) - aggregating module that contains no code on its own, only project definitions
 |- phoenix              - everything that was in root project; our phoenix monolith
 |- core                 - core framework, core utils and starfish
 |- objectframework      - form/shadow stuff: models, illuminators and other utils
```

To clean up empty directories left behind, use `git clean -dn` (dry run) and replace `n` with `i` for interactive mode or `f` to force cleanup.    
Consult `git help clean` to see if passing `-x` or `-X` arguments can be benefitial for you.

### Packages and imports
- `phoenix`: everything that was in the root package is now moved to `phoenix` package. So, for example, `import models.Foo` is now `import phoenix.models.Foo`
- `core`: package contains all the base stuff you want, like `import core.db._` or `import core.utils.blah`
- `objectframework`: package contains all the classes/objects appropriate to its definition :)

There are no further major changes planned to this. I will continue working on moving subprojects out of phoenix, but that won't change the base structure.

## Library updates
Slick version was bumped, so now `import slick.driver.PostgresDriver.api._` is replaced with `import slick.jdbc.PostgresProfile.api._`

## Style guide
We have finally started a Scala style guide for phoenix: https://github.com/FoxComm/highlander/wiki/Phoenix-Scala-style-guide    
Please check it out and feel free to start a discussion in #phoenix Slack channel on any of rules covered or missing.    
The main reason for this style guide is not just how we would like our code to look, but the amount of bugs caused by our current coding style.    
In this initial verison, we cover default and optional arguments in methods and data types. They are now considered harmful because it's super easy to overlook them and forget to override. Please provide values explicitly at call sites!

_Happy hacking!    
-- Anna_

-----
