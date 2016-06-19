Isaac
=========================

Isaac is a prototype auth service which validates JWT tokens quickly. 
Maybe it will generate them one day too. 

Validation
=========================

The JWT token's signature and the user the token refers to is validated. If the
user does not exist, then the token is invalid. The JWT token will also have an
integer called a "ratchet" which will increment whenever a user requires previous
JWT tokens to be invalidated.
