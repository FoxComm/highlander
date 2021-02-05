let

  nixpkgs = (import <nixpkgs> {}).pkgs.fetchFromGitHub {
    owner = "NixOS"; repo = "nixpkgs";
    rev = "0011f9065a1ad1da4db67bec8d535d91b0a78fba"; # nixos-unstable
    sha256 = "0m662mibyxqmp83zdhsi084p2h90268h3i8bfk3b2q8pbjz89yx2";
  };

in with import nixpkgs {}; {
  main = let
    dnsimple = python27Packages.buildPythonPackage rec {
      name = "dnsimple-${version}";
      version = "0.3.6";

      src = pkgs.fetchurl {
        url = "mirror://pypi/d/dnsimple/${name}.tar.gz";
        sha256 = "0cc7v6wn246n3gjpzy8aq8v3xbrxl9a423b84i2z2cjjbq8b7fvf";
      };

      propagatedBuildInputs = [ python27Packages.requests ];
    };
  in (buildFHSUserEnv {
    name = "main";
    targetPkgs = pkgs: (with pkgs; [ ansible2 curl dnsimple docker gnumake git google-cloud-sdk openssh python27Full python27Packages.libcloud python27Packages.pycrypto python27Packages.requests rsync ] ++ python27Packages.libcloud.propagatedBuildInputs ++ python27Packages.pycrypto.propagatedBuildInputs ++ dnsimple.propagatedBuildInputs);
    profile = ''
      source .env.local;
      export PYTHONPATH="/usr/lib/python2.7/site-packages"
    '';
  }).env;

  api-docs = stdenv.mkDerivation {
    name = "api-docs";
    buildInputs = [ gcc pkgconfig nodejs-6_x ];
    shellHook = ''
      source .env.local
      cd api-docs
    '';
  };

  ashes = let
    flow25 = stdenv.lib.overrideDerivation flow (oldAttrs : rec {
      name = "flow-${version}";
      version = "0.25.0";

      src = fetchFromGitHub {
        owner = "facebook";
        repo = "flow";
        rev = "v${version}";
        sha256 = "1v82phl1cb10p0ggjq9w6a0bcysjxwgmlqsp9263n3j52dl74yi8";
      };
    });
  in stdenv.mkDerivation {
    name = "ashes";
    buildInputs = [ gcc pkgconfig fontconfig yarn cairo libpng pixman nodejs-6_x flow25 ];
    shellHook = ''
      source .env.local
      cd ashes
      ln -sf `readlink -f $(which flow)` node_modules/flow-bin/vendor/flow
    '';
  };

  green-river = stdenv.mkDerivation {
    name = "green-river";
    buildInputs = [ openjdk sbt ];
    shellHook = ''
      source .env.local
      cd green-river
    '';
  };

  middlewarehouse = stdenv.mkDerivation {
    name = "middlewarehouse";
    buildInputs = [ go glide ];
    shellHook = ''
      source .env.local
      cd middlewarehouse
    '';
  };

  phoenix = stdenv.mkDerivation {
    name = "phoenix";
    buildInputs = [ openjdk sbt ];
    shellHook = ''
      source .env.local
      cd phoenix-scala
    '';
  };
}
