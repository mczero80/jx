# -*-Perl-*-
# Version 1.0: Michael Golm
# Extracts the symbol information from an ELF32 executable
# using "objdump" and creates an include file containing
# the symbols and addresses.
# Used in the VM to find native method code.

if (@ARGV != 2) {
    print "Usage: perl mksymtab.perl <elf32executable> <headerfilename>\n"; 
    exit 1; 
}

$executable = @ARGV[0];
$header = @ARGV[1];

if (! -e $executable) {
  $createEmpty = 1;
} else {
# open(INFILE, "objdump --syms $executable | grep .text |");
# remove duplicates
  open(INFILE, "objdump --syms $executable | grep .text | sort +5 -u |");
}
open(OUTFILE, ">$header");

print OUTFILE "/* DO NOT MODIFIY - AUTOMATICALLY GENERATED FILE */\n";
print OUTFILE "/* FILE_CAN_BE_DELETED_AT_ANY_TIME */\n\n";
print OUTFILE "#ifndef SYMBOLS_H\n";
print OUTFILE "#define SYMBOLS_H\n\n";
print OUTFILE "struct symbols_s {\n";
print OUTFILE "  unsigned long name;\n";
print OUTFILE "  unsigned long addr;\n";
print OUTFILE "  unsigned long size;\n";
print OUTFILE "};\n\n";

$strings = "";
$idx = 0;
$nsymbols = 0;
if (! $createEmpty) {
  while (<INFILE>) {
    ($addr, $g, $F, $text, $size, $name) = split(/\s+/);
    next if ($F ne "F");
    if ($name eq ".hidden" ) {$name = "_hidden";}
    print OUTFILE "#define FKTSIZE_$name 0x$size\n";
    print OUTFILE "#define FKTADDR_$name 0x$addr\n";
    $strings .= "$name\\0";
    $symbols .= "{ $idx, 0x$addr, 0x$size },\n";
    $idx += length($name) + 1;
    $nsymbols++;
  }
  close INFILE; 
} else {
  print OUTFILE "#define FKTSIZE_EMPTY\n\n";
  if (-e "symbols.autodef") {
    open(DEFFILE, "symbols.autodef");
    print OUTFILE "/* included defines from symbols.autodef */\n";
    while (<DEFFILE>) {
      print OUTFILE "$_";
    }
    close(DEFFILE);
  } else {
    print OUTFILE "/* symbols.autodef didn`t exist */\n";
  }
}

print OUTFILE "\nstatic char strings[] __attribute__ ((unused)) = \"";
print OUTFILE "$strings\";\n\n";
print OUTFILE "static struct symbols_s symbols[] __attribute__ ((unused)) = {";
print OUTFILE "$symbols\n};\n";
print OUTFILE "static int n_symbols __attribute__ ((unused)) = $nsymbols;\n";
print OUTFILE "#endif\n";
close OUTFILE;
