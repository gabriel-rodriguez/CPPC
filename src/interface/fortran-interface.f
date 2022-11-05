c    This file is part of CPPC.
c
c    CPPC is free software; you can redistribute it and/or modify
c    it under the terms of the GNU General Public License as published by
c    the Free Software Foundation; either version 2 of the License, or
c    (at your option) any later version.
c
c    CPPC is distributed in the hope that it will be useful,
c    but WITHOUT ANY WARRANTY; without even the implied warranty of
c    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
c    GNU General Public License for more details.
c
c    You should have received a copy of the GNU General Public License
c    along with CPPC; if not, write to the Free Software
c    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA



      SUBROUTINE CPPCF_OPEN( LUNIT, PATH, CPPC_FILE_CODE )
      IMPLICIT NONE

      INTEGER*4 CPPC_FILE_CODE
      INTEGER*4 LUNIT, RESTART_TEST, OFFSET
      CHARACTER*256 PATH
      CHARACTER*256 PATH_NP
      INTEGER*4 CPPCF_LENGTH

c     IT COULD HAPPEN THAT 'PATH' IS A CONSTANT, WHICH COULD RAISE
c     A SEGMENTATION FAULT IF OVERWRITTEN. WE USE A LOCAL VARIABLE
c     AS A WORKAROUND, JUST IN CASE.
      CALL CPPC_JUMP_NEXT( RESTART_TEST )

      IF( RESTART_TEST .EQ. 1 ) THEN

c       OBTAIN PATH AND OFFSET FOR THIS FILE CODE
        CALL CPPC_REGISTER_DESCRIPTOR( CPPC_FILE_CODE,
     +    0, 0, PATH_NP, OFFSET )

c       FILE NOT FOUND IN CHECKPOINT
        IF( OFFSET .EQ. -1 ) THEN
          RETURN
        END IF

c       OPEN SAID PATH
        OPEN( LUNIT, FILE=PATH_NP )

c       POSITION THE OFFSET
        CALL FSEEK( LUNIT, OFFSET, 0 ) ! 0 goes for absolute

c       UPDATE FORTRAN UNIT AND FILE DESCRIPTOR
        CALL CPPC_UPDATE_DESCRIPTOR( CPPC_FILE_CODE,
     +    FNUM( LUNIT ), LUNIT, PATH_NP,
     +    OFFSET )
      ELSE
        PATH_NP = PATH//'\0'
        CALL CPPC_REGISTER_DESCRIPTOR( CPPC_FILE_CODE,
     +    FNUM( LUNIT ), LUNIT, PATH_NP, OFFSET )
      END IF

      END SUBROUTINE

      SUBROUTINE CPPCF_CLOSE( LUNIT )
      IMPLICIT NONE

      INTEGER*4 LUNIT

c     UNREGISTER THIS LOGICAL UNIT
      CALL CPPC_UNREGISTER_DESCRIPTOR( LUNIT,
     +  FNUM( LUNIT ) )

      END SUBROUTINE

      SUBROUTINE CPPCF_DO_CHECKPOINT( CODE )
      IMPLICIT NONE

      INTEGER*4 CODE, CURRENT_UNIT
      INTEGER*4 OFFSET, CURRENT_CODE
      INTEGER*4 CPPC_JUMP_TEST

      CALL CPPC_JUMP_NEXT( CPPC_JUMP_TEST )

      IF( CPPC_JUMP_TEST .EQ. 0 ) THEN
        CURRENT_UNIT = -1
        CALL CPPC_NEXT_UNIT( CURRENT_UNIT, CURRENT_CODE )
        DO WHILE( CURRENT_UNIT .NE. -1 )
          CALL FTELL( CURRENT_UNIT, OFFSET )
          CALL CPPC_OFFSET_SET( CURRENT_CODE, OFFSET )
          CALL CPPC_NEXT_UNIT( CURRENT_UNIT, CURRENT_CODE )
        END DO
      END IF

      CALL CPPC_DO_CHECKPOINT( CODE )

      END SUBROUTINE
