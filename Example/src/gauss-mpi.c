#include <mpi.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define OUT_FORMAT "%+.8le\n"

int digitos(int n)
{
  int aux,cifras=1;

  aux=n;
  while((aux/10)!=0)
    cifras++;

  return(cifras);
}

char *itoa(int n)
{
  int aux,cifras,i;
  char *ret;

  cifras=digitos(n);

  ret=(char *)malloc(sizeof(char)*cifras);

  aux=n;
  for(i=cifras-1;i>=0;i--)
    {
      ret[i]='0'+(aux%10);
      aux/=10;
    }

  return(ret);
}

int tagfactor(int fila)
{
  return(fila*10+0);
}

int tagfila(int fila)
{
  return(fila*10+1);
}

char *grupofila(int g)
{
  char *ret=(char *)malloc(sizeof(char)*3);

  sprintf(ret,"F%d",g);

  return(ret);
}

char *grupocolumna( int g ) {

  char *ret=(char *)malloc(sizeof(char)*3);

  sprintf(ret,"C%d",g);

  return(ret);
}

int main(int argsno,char **args)
{
  int mitid,filmalla,colmalla,dim,i,j,numhijos;
  char *fmatA,*fmatB,*fmatX;
  int fmatXlen;
  int nfil,ncol,misfilasn,miscolumnasn,countc,countf,aux_rank;
  double *A,*b,*factor;
  FILE *matA,*matB,*matX;
  MPI_Comm grupofila,grupocolumna,grupoB;
  int atam;

  if( MPI_Init(&argsno,&args) != MPI_SUCCESS ) {
      perror("Can't initialize MPI: ");
      exit(0);
  }
  
  if(argsno!=6)  {
      printf( "Syntax error\n" ); 
      printf( "Usage: gauss <dimA> <dimB> <fich1> <fich2> <fich3>\n" ); 
      printf( "<dimA> : Rows in the grid of processors\n" ); 
      printf( "<dimB> : Columns in the grid of processors\n" ); 
      printf( "<fich1>: File containing system matrix\n" ); 
      printf( "<fich2>: File containing independent term matrix\n" ); 
      printf( "<fich3>: Output file\n");
      MPI_Finalize();

      exit(0);
  }
  
  filmalla=atoi(args[1]);
  colmalla=atoi(args[2]);
  fmatA=args[3];
  fmatB=args[4];
  fmatX=args[5];

  fmatXlen = strlen( fmatX ) + 1;

  if((matA=fopen(fmatA,"r"))==NULL)
    {
      printf("Error: can't open file %s\n",fmatA);
      exit(0);
    }
  if((matB=fopen(fmatB,"r"))==NULL)
    {
      printf("Error: can't open file %s\n",fmatB);
      exit(0);
    }

  
  fscanf(matA,"%d %d\n",&dim,&dim);
  fscanf(matB,"%d\n",&i);

  if(dim!=i) {
      printf("Matrix dimensions are not valid\n");
      exit(0);
  }

  /* Reduction of the number of processors */
  if(filmalla>dim)
    filmalla=dim;
  if(colmalla>dim)
    colmalla=dim;

  MPI_Comm_rank(MPI_COMM_WORLD,&mitid);
  MPI_Comm_size(MPI_COMM_WORLD,&numhijos);
  
  if(mitid==0) {
	  
      if( numhijos != filmalla*colmalla ) {
	  printf( "Number of processes, rows and columns don't coincide\n" );
	  printf( "Rows: %d. Columns: %d. Processes: %d\n", filmalla, colmalla, numhijos );

	  MPI_Finalize();
	  exit(0);
      }

      /* Remove the output file to overwrite it */
      unlink(fmatX);
  }
  
  MPI_Barrier(MPI_COMM_WORLD);
  
  nfil=mitid / colmalla;
  ncol=mitid % colmalla;

  if(nfil<(dim%filmalla))
    misfilasn=dim/filmalla+1;
  else
    misfilasn=dim/filmalla;

  if(ncol<(dim%colmalla))
    miscolumnasn=dim/colmalla+1;
  else
    miscolumnasn=dim/colmalla;

  MPI_Comm_split(MPI_COMM_WORLD, nfil, mitid, &grupofila);
  MPI_Comm_split(MPI_COMM_WORLD, ncol, mitid, &grupocolumna);
  MPI_Comm_split(MPI_COMM_WORLD, ncol == dim%colmalla, mitid, &grupoB);

  A = (double *)malloc(sizeof(double)*misfilasn*miscolumnasn);
  b=(double *)malloc(sizeof(double)*misfilasn);
  factor=(double *)malloc(sizeof(double)*misfilasn);

  atam = misfilasn * miscolumnasn;

  countf=0;  
  for(i=0;i<dim;i++)
    {
      int leido;
      double trash;

      leido=0;
      countc=0;
      for(j=0;j<dim-1;j++)
	{
	  if(((i%filmalla)==nfil)&&((j%colmalla)==ncol))
	    {
	      fscanf(matA,"%lf ",&(A[countf*misfilasn+countc++]));
	      leido=1;
	    }
	  else	    
	    fscanf(matA,"%lf ",&trash);
	}

      if(((i%filmalla)==nfil)&&(((dim-1)%colmalla)==ncol))
	{
	  fscanf(matA,"%lf\n",&(A[countf*misfilasn+miscolumnasn-1]));
	  leido=1;
	}
      else	    
	fscanf(matA,"%lf\n",&trash);

      if(ncol==dim%colmalla)
	{
	  if(((i%filmalla)==nfil))
	    fscanf(matB,"%lf\n",&(b[countf]));
	  else
	    fscanf(matB,"%lf\n",&trash);
	}


      if(leido)
	countf++;
    }

  for(i=0;i<dim;i++)
    {
      double pivote,*filapivote,bpivote;

      if(mitid==0)
	printf("Iteration %d\n",i);

      filapivote=(double *)malloc(sizeof(double)*miscolumnasn);
     
      /* The elements with the factors calculate them. The others receive them. */
      if(i%colmalla==ncol)
	{
	  for(j=0;j<misfilasn;j++)
	    factor[j]=A[j*misfilasn+i/colmalla];

	  /* If the pivot is too small, stop */
	  if(i%filmalla==nfil)
	    if(fabs(A[i/filmalla*misfilasn+i/colmalla])<1E-5)
	      {
		printf("Bad pivot\n");

		MPI_Finalize();
		exit(0);
	      }
	}

      MPI_Comm_rank(grupofila,&aux_rank);
      MPI_Bcast(factor,misfilasn,MPI_DOUBLE,i%colmalla,grupofila);
     
      /* Elements in the pivot's row divide and send */
      if(i%filmalla==nfil)
	{
	  pivote=factor[i/filmalla];
	  for(j=0;j<miscolumnasn;j++)
	    {
	      A[i/filmalla*misfilasn+j]/=pivote;
	      filapivote[j]=A[i/filmalla*misfilasn+j];
	    }

	  /* Processes with b pack it along with the row */
	  if(dim%colmalla==ncol)
	    {
	      b[i/filmalla]/=pivote;
	      bpivote=b[i/filmalla];
	    }
	}
      
      if(dim%colmalla==ncol)
	{
	  double *buffer;
	  int k;
          buffer = (double *)malloc( sizeof(double)*(miscolumnasn+1) );
	  for(k=0;k<miscolumnasn;k++)
	    buffer[k]=filapivote[k];
	  buffer[miscolumnasn]=bpivote;

	  MPI_Bcast(buffer,miscolumnasn+1,MPI_DOUBLE,i%filmalla,grupocolumna);

	  for(k=0;k<miscolumnasn;k++)
	    filapivote[k]=buffer[k];
	  bpivote=buffer[miscolumnasn];
          free( buffer );
	}
      else
	MPI_Bcast(filapivote,miscolumnasn,MPI_DOUBLE,i%filmalla,grupocolumna);

      /*  Calculation */
      for(j=0;j<misfilasn;j++)
	if(nfil+j*filmalla!=i)
	  {
	    int k;
	    
	    for(k=0;k<miscolumnasn;k++)
	      A[j*misfilasn+k]-=factor[j]*filapivote[k];

	    /* Processors with b calculate it */
	    if(ncol==dim%colmalla)
	      b[j]-=factor[j]*bpivote;
	  }

      free(filapivote);
    }

  /* Output result */
  if( (dim % colmalla) == ncol ) {

      int fd;

      fd = open( fmatX, O_RDWR );
      while( ( fd = open( fmatX, O_RDWR|O_CREAT, 0777 ) ) == -1 );
      {
        double trash;

        matX=fdopen(fd,"r+");
	if( fscanf( matX, "%lf\n", &trash) == EOF ) {

	  double d = 0;

	  fprintf( matX, "%d\n", dim );
	  for( i = 0; i < dim; i++ ) {
	    fprintf( matX, OUT_FORMAT, d );
	  }
	  rewind( matX );
	  fscanf( matX, "%lf\n", &trash );
	}

	for( i = 0; i < dim; i++ ) {
	  if( i % filmalla == nfil ) {
	    fprintf( matX, OUT_FORMAT, b[ i / filmalla ] );
	  } else {
	    fscanf( matX, "%lf\n", &trash );
	  }
	}
      }

      fclose( matX );
      close( fd );
  }

  MPI_Finalize();
  return( 0 );
}
