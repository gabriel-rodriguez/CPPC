#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

int main(int argsno,char **args)
{
  int nfil,ncol,i,j;
  char *p_out;
  FILE *f_out;
  struct timeval *time = (struct timeval *)malloc(sizeof(struct timeval));
  struct timezone *zone = (struct timezone *)malloc(sizeof(struct timezone));
  
  if(argsno != 4)
    {
      printf("Error de sintaxis\n");
      printf("Uso: matgen <nfil> <ncol> <salida>\n");
      exit(0);
    }

  gettimeofday(time,zone);
  srand(time->tv_usec);

  nfil = atoi(args[1]);
  ncol = atoi(args[2]);
  p_out = args[3];

  f_out = fopen(p_out,"w");

  if(nfil!=1)
    fprintf(f_out,"%d %d\n",nfil,ncol);
  else
    fprintf(f_out,"%d\n",ncol);

  for(i=0;i<nfil;i++)
    {
      for(j=0;j<ncol;j++)
	fprintf(f_out,"%d ",rand());
      fprintf(f_out,"\n");
    }

  fclose(f_out);
  free(time);
  free(zone);

  return(0);
}
