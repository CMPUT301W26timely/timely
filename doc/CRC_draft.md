
### Class: User

###### Responsibilities

- Represents each user of the app

###### Collaborators

- Entrant
- Organizer
- Admin


### Class: Entrant

###### Responsibilities

- Holds info about an Entrant

###### Collaborators

- WaitList
- Event
- Organizer

### Class: Event

###### Responsibilities

- Holds info about event

###### Collaborators

- WaitingList
- Entrant
- Organizer



### Class: WaitingList

###### Responsibilities

- Contains a list of wait entrants for the event

###### Collaborators

- Entrant
- Event
- Organizer



### Class: Organizer

###### Responsibilities

- Creates and manages events

###### Collaborators

- Entrant
- Events
- WaitingList



### Class: Administrator

###### Responsibilities

- Controls events listed, profiles, images, organizers

###### Collaborators

- Events
- Notifications
- Collaborators
- Profile


### Class: Profile

###### Responsibilities

- Holds info such as name, email and optional phone number

###### Collaborators

- Entrant
- Administrator
- Role


### Class: Notification  

##### Responsibilities

- Holds notification data

##### Collaborators

- Administrator
- Entrant
- Organizer


### Class: Map

###### Responsibilities

- Display location info of entrants

###### Collaborators

- Entrants
- Organizer


### Class: LotteryService

###### Responsibilities

- Randomly select N entrants from the waiting list 
- re-draw if someone declines

###### Collaborators

- Event
- WaitingList
- Entrant
- Notification


### Class: Role

###### Responsibilities

- Parent class for Entrant, Organizer, Admin

###### Collaborators

- Entrant
- Organizer
- Admin
- User


### Class: QRCode

###### Responsibilities

- Link to Event, Generate URL

###### Collaborators

- Event
- Organizer
- User


### Class: NotificationService

###### Responsibilities

- sends push notifications

###### Collaborators

- Notification
- User


### Class: Payment

###### Responsibilities

- Track payment status

###### Collaborators

- Event
- User