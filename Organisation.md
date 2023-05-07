Summary of what each team member did and how the team was organised. For example, the team may discuss the domain model together but only one person implements. This will mean we might only see commits from that person, and wonder if the other members were involved. A short explanation in Organisation.md will help this. It could be a couple of sentences for each member or a simple All members participated about the same for everything (in which case the logs should reflect this).

1. The roles of each team member were delegated during an online meeting in which we discussed our individual work loads and we selected tasks based on who was more confident on what and also who would be available to work on the assignment during times of the week.
The final task delegation was planned and executed as follows:

- Domain model: Josephine was tasked with implementing this, but the whole team worked together (as shown in the issues discussions) to dicuss the organisation of the domain model and what would be required in order to create a domain model that was an accurate representation of the relationships between the different entities and also used the appropriate fetch types and cascade options to optimize performance and maintain data integrity.
- Setting up - While Josephine implemented the domain model, Thireshand and Medhavi worked on getting the Tomcat server running in order to run the webapp. They also worked on trying to fix the 404 error with the tests which they solved from searching on Piazza.
- Concert and Performer services - Josephine was tasked with working on the simple retrival of the concerts and performers.
- Thireshan and Medhavi were tasked with working on the bulk of the booking services and authetication. They mostly worked together for most of the project on a voice call as this was an efficient way to collaborate.
- Mapper classes: Thireshan took the initiative to work on the Mapper classes so that the booking services could then be worked on. 
- Authentication: Thireshan and Medhavi mostly worked together on the authentication services.   
- Booking Services: Thireshan and Medhavi mostly worked together on the booking services.   
- Subscription and notification services - Josephine was tasked with working on these services.
- Refactoring - Medhavi and Thireshan worked on pulling the final working code and tidying up the files plus making the code more readable.

Are team communicated mostly through Discord for simple communications. We also used Issues to report more important events, record more important notes and record major/difficult errors that we were stuck on. We found voice calls to be the most efficient way to help each other as we were able to share screen and be able to see what other team members were doing live.


2. Short description of the strategy used to minimise the chance of concurrency errors in program execution (2-3 sentences)

Strategies we used to minimise concurrency errors in the program execution were Pessismistic/Optimistic locking and Transaction management of the EntityManager.
We used pessimistic locking such as LockModeType.PESSIMISTIC_READ and LockModeType.PESSIMISTIC_WRITE to ensure that transactions cannot make changes over one another until the first transaction has completed; likewise with optimistic locking which we roll back a transaction if an exception is thrown. Transaction management to begin and commit transactions were used to ensure that transactions are atomic and can also be rolled back when an exception or conflict occurs.

3. Short description of how the domain model is organised (2-3 sentences)

The domain model of the concert booking web app is organised into five JPA entities: Concert, Performer, Reservation, Seat, and User, each mapped to a corresponding database table using JPA annotations. We connected the entities through well-defined relationships, using various annotations such as @OneToMany, @ManyToOne (Reservation - User), @OneToOne (Reservation - Seat), and @ManyToMany (Concert - Performer), alongside FetchType and CascadeType options to optimize data retrieval and manage entity state transitions. This organisation allowed for a structured representation of the concert booking process while also ensuring efficient data access.
