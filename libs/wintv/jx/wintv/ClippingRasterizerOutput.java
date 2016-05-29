package jx.wintv;

interface ClippingRasterizerOutput {
   void write(int width, boolean flag_sol, boolean flag_eol);
   void skip(int width, boolean flag_sol, boolean flag_eol);
   void eof();
}

