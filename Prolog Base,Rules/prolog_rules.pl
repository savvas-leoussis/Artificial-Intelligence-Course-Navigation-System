canMoveFromTo(X, Y, R) :-  pointRoadPosition(X, L, P1),  pointRoadPosition(Y, L, P2),  P2 > P1,  \+ forbidden(L),  oneway(L,1), R is L. 
canMoveFromTo(X, Y, R) :-  pointRoadPosition(X, L, P1),  pointRoadPosition(Y, L, P2),  P1 > P2,  \+ forbidden(L),  oneway(L,-1), R is L. 
canMoveFromTo(X, Y, R) :-  pointRoadPosition(X, L, P1),  pointRoadPosition(Y, L, P2),  \+ forbidden(L),  twoway(L), R is L. 
morning(T) :- T > 8, T < 12.
noon(T) :- T > 12, T < 16.
evening(T) :- T > 16, T < 20.
priority(R, T, Z) :-   (highway(R, ValueHighway) -> RES1 is ValueHighway ; RES1 is 0 ),   (morning(T) -> (current_traffic(R, 9-11, ValueM) -> RES2 is ValueM ; RES2 is 0 )),   (noon(T) -> (current_traffic(R, 13-15, ValueN) -> RES3 is ValueN ; RES3 is 0 )),   (evening(T) -> (current_traffic(R, 17-19, ValueE) -> RES4 is ValueE ; RES4 is 0 )),  Z is RES1 + RES2 + RES3 + RES4, !. 
