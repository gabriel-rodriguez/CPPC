#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define OUT_FORMAT "%+.8le\n"

int main(int argsno,char **args)
{
  int dim, i, j;
  char *fmatA,*fmatB,*fmatX;
  double *A,*b;
  FILE *matA,*matB,*matX;

  if(argsno!=4)  {
      printf( "Syntax error\n" ); 
      printf( "Usage: gauss <fich1> <fich2> <fich3>\n" ); 
      printf( "<fich1>: File containing system matrix\n" ); 
      printf( "<fich2>: File containing independent term matrix\n" ); 
      printf( "<fich3>: Output file\n");
      exit(-1);
  }
  
  fmatA=args[1];
  fmatB=args[2];
  fmatX=args[3];

  if((matA=fopen(fmatA,"r"))==NULL) {
    perror( fmatA );
    exit(0);
  }
  if((matB=fopen(fmatB,"r"))==NULL) {
    perror( fmatB );
    exit(0);
  }
  
  fscanf(matA,"%d %d\n",&dim,&dim);
  fscanf(matB,"%d\n",&i);

  if(dim!=i) {
    printf("Matrix dimensions are not valid\n");
    exit(0);
  }

  /* Remove the output file to overwrite it */
  unlink(fmatX);
  if( (matX = fopen( fmatX, "w") ) == 0 ) {
    perror( fmatX );
    exit( 0 );
  }
  
  A = (double *)malloc(sizeof(double)*dim*dim);
  b=(double *)malloc(sizeof(double)*dim);

  for(i=0;i<dim;i++) {
    for(j=0;j<dim;j++) {
      fscanf( matA, "%lf ", &A[i*dim+j] );
    }

    fscanf( matB, "%lf\n", &b[i] );
  }

  for(i=0;i<dim;++i) {
    double p;
    printf("Iteration %d\n",i);

    if( fabs( A[i*dim+i] ) < 1E-5 ) {
      printf( "Bad pivot: aborting\n" );
      free(A);
      free(b);
      exit(-1);
    }

    p = A[i*dim+i];
    for( j = 0; j < dim; ++j ) {
      A[ i*dim+j ] /= p;
    }
    b[i] /= p;

    for( j = 0; j < dim; ++j ) {
      double f;
      int k;

      if( j != i ) {
        f = A[j*dim+i];
        for( k = 0; k < dim; ++k ) {
	  A[j*dim+k] -= f * A[i*dim+k];
        }
        b[j] -= f * b[i];
      }
    }
  }

  fprintf( matX, "%d\n", dim );
  for( i = 0; i < dim; ++i ) {
    fprintf( matX, "%+.8le\n", b[i] );
  }

  free(A);
  free(b);
  return( 0 );
}
