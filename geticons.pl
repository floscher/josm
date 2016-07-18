#! /usr/bin/perl -w
# short tool to find out all used icons and allows deleting unused icons
# when building release files

my @default = (
  "styles/standard/*.xml",
  "styles/standard/*.mapcss",
  "data/*.xml",
  "src/org/openstreetmap/josm/*.java",
  "src/org/openstreetmap/josm/*/*.java",
  "src/org/openstreetmap/josm/*/*/*.java",
  "src/org/openstreetmap/josm/*/*/*/*.java",
  "src/org/openstreetmap/josm/*/*/*/*/*.java",
  "src/org/openstreetmap/josm/*/*/*/*/*/*.java"
);

my %icons;

my $o = $/;

for my $arg (@ARGV ? @ARGV : @default)
{
  for my $file (glob($arg))
  {
    open(FILE,"<",$file) or die "Could not open $file\n";
    #print "Read file $file\n";
    $/ = $file =~ /\.java$/ ? ";" : $o;
    my $extends = "";
    while(my $l = <FILE>)
    {
      next if $l =~ /NO-ICON/;
      if($l =~ /icon\s*[:=]\s*["']([^"'+]+?)["']/)
      {
        ++$icons{$1};
      }

      if(($l =~ /(?:icon-image|repeat-image|fill-image)\s*:\s*(\"?(.*?)\"?)\s*;/) && ($1 ne "none"))
      {
        my $val = $2;
        my $img = "styles/standard/$val";
        $img = "styles/$val" if((!-f "images/$img") && -f "images/styles/$val");
        $img = $val if((!-f "images/$img") && -f "images/$val");
        ++$icons{$img};
      }
      if($l =~ /ImageProvider(?:\.get)?\(\"([^\"]*?)\"(?:, ImageProvider.ImageSizes.[A-Z]+)?\)/)
      {
        my $i = $1;
        $i = "styles/standard/$i" if $i eq "misc/no_icon";
        ++$icons{$i};
      }
      while($l =~ /\/\*\s*ICON\s*\*\/\s*\"(.*?)\"/g)
      {
        my $i = $1;
        ++$icons{$i};
      }
      while($l =~ /\/\*\s*ICON\((.*?)\)\s*\*\/\s*\"(.*?)\"/g)
      {
        my $i = "$1$2";
        ++$icons{$i};
      }
      if($l =~ /new\s+ImageLabel\(\"(.*?)\"/)
      {
        my $i = "statusline/$1";
        ++$icons{$i};
      }
      if($l =~ /createPreferenceTab\(\"(.*?)\"/)
      {
        my $i = "preferences/$1";
        ++$icons{$i};
      }
      if($l =~ /setIcon\(\"(.*?)\"/)
      {
        my $i = "statusline/$1";
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.get(?:IfAvailable)?\(\"(.*?)\",\s*\"(.*?)\"\s*\)/)
      {
        my $i = "$1/$2";
        ++$icons{$i};
      }
      if($l =~ /new ImageProvider\(\"(.*?)\",\s*\"(.*?)\"\s*\)/)
      {
        my $i = "$1/$2";
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.overlay\(.*?,\s*\"(.*?)\",/)
      {
        my $i = $1;
        ++$icons{$i};
      }
      if($l =~ /getCursor\(\"(.*?)\",\s*\"(.*?)\"/)
      {
        my $i = "cursor/modifier/$2";
        ++$icons{$i};
        $i = "cursor/$1";
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.getCursor\(\"(.*?)\",\s*null\)/)
      {
        my $i = "cursor/$1";
        ++$icons{$i};
      }
      if($l =~ /SideButton*\(\s*(?:mark)?tr\s*\(\s*\".*?\"\s*\)\s*,\s*\"(.*?)\"/)
      {
        my $i = "dialogs/$1";
        ++$icons{$i};
      }
      if($l =~ /super\(\s*tr\(\".*?\"\),\s*\"(.*?)\"/s)
      {
        my $i = "$extends$1";
        ++$icons{$i};
      }
      if($l =~ /super\(\s*trc\(\".*?\",\s*\".*?\"\),\s*\"(.*?)\"/s)
      {
        my $i = "$extends$1";
        ++$icons{$i};
      }
      if($l =~ /audiotracericon\",\s*\"(.*?)\"/s)
      {
        my $i = "markers/$1";
        ++$icons{$i};
      }
      if($l =~ /\"(.*?)\",\s*parentLayer/s)
      {
        my $i = "markers/$1";
        ++$icons{$i};
      }
      if($l =~ /setButtonIcons.*\{(.*)\}/)
      {
        my $t = $1;
        while($t =~ /\"(.*?)\"/g)
        {
          my $i = $1;
          ++$icons{$i};
        }
      }
      if($l =~ /extends MapMode/)
      {
        $extends = "mapmode/";
      }
      elsif($l =~ /extends ToggleDialog/)
      {
        $extends = "dialogs/";
      }
      elsif($l =~ /extends JosmAction/)
      {
        $extends = "";
      }
    }
    close FILE;
  }
}

my %haveicons;

for($i = 1; my @ifiles = (glob("images".("/*" x $i).".png"), glob("images".("/*" x $i).".svg")); ++$i)
{
  for my $ifile (sort @ifiles)
  {
    $ifile =~ s/^images\///;
    # svg comes after png due to the glob, so only check for svg's
    if($ifile =~ /^(.*)\.svg$/)
    {
      if($haveicons{"$1.png"})
      {
        print STDERR "File $1 exists twice as .svg and .png.\n";
      }
      # check for unwanted svg effects
      if(open FILE, "<","images/$ifile")
      {
        undef $/;
        my $f = <FILE>;
        close FILE;
        while($f =~ /style\s*=\s*["']([^"']+)["']/g)
        {
          for my $x (split(/\s*;\s*/, $1))
          {
            print STDERR "Style starts with minus for $ifile: $x\n" if $x =~ /^-/;
          }
        }
        if($f =~ /viewBox\s*=\s*["']([^"']+)["']/)
        {
          my $x = $1;
          print STDERR "ViewBox has float values for $ifile: $x\n" if $x =~ /\./;
        }
      }
      else
      {
        print STDERR "Could not open file $ifile: $1";
      }
    }
    $haveicons{$ifile} = 1;
  }
}

for my $img (sort keys %icons)
{
  if($img =~ /\.(png|svg)/)
  {
    print STDERR "File $img does not exist!\n" if(!-f "images/$img");
    delete $haveicons{$img};
  }
  else
  {
    print STDERR "File $img(.svg|.png) does not exist!\n" if(!-f "images/$img.png" && !-f "images/$img.svg");
    delete $haveicons{"$img.svg"};
    delete $haveicons{"$img.png"};
  }
}

for my $img (sort keys %haveicons)
{
  print "$img\n";
}
