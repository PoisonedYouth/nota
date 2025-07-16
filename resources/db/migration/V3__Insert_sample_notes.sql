-- Insert sample notes for testing and demonstration
INSERT INTO notes (title, content, due_date, created_at, updated_at, archived, archived_at, user_id)
VALUES
    ('Welcome to Nota',
     'This is your first note! Nota is a simple and efficient note-taking application. You can create, edit, archive, and organize your notes easily.',
     NULL,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Shopping List',
     'Groceries to buy:
- Milk
- Bread
- Eggs
- Apples
- Chicken breast
- Rice
- Vegetables for salad',
     CURRENT_TIMESTAMP + INTERVAL '2' DAY,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Meeting Notes - Project Planning',
     'Key points from today''s meeting:
- Project deadline: End of next month
- Team assignments completed
- Need to review budget allocation
- Schedule follow-up meeting for next week
- Action items assigned to team members',
     CURRENT_TIMESTAMP + INTERVAL '7' DAY,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Book Recommendations',
     'Books to read:
1. "The Clean Coder" by Robert C. Martin
2. "Effective Java" by Joshua Bloch
3. "Spring in Action" by Craig Walls
4. "Kotlin in Action" by Dmitry Jemerov
5. "Clean Architecture" by Robert C. Martin',
     NULL,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Workout Plan',
     'Weekly exercise routine:
Monday: Upper body strength training
Tuesday: Cardio (30 min run)
Wednesday: Lower body strength training
Thursday: Rest day or light yoga
Friday: Full body workout
Weekend: Outdoor activities or sports',
     NULL,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Travel Ideas',
     'Places I want to visit:
- Japan (Tokyo, Kyoto, Osaka)
- Iceland (Northern Lights, Blue Lagoon)
- New Zealand (Hobbiton, Milford Sound)
- Norway (Fjords, Bergen)
- Costa Rica (Rainforests, Beaches)',
     NULL,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Completed Project Ideas',
     'Ideas that were implemented:
- Personal blog website
- Task management app
- Recipe collection database
- Photo gallery application
- Weather dashboard',
     NULL,
     CURRENT_TIMESTAMP - INTERVAL '30' DAY,
     CURRENT_TIMESTAMP - INTERVAL '15' DAY,
     TRUE,
     CURRENT_TIMESTAMP - INTERVAL '15' DAY,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Old Shopping List',
     'Items that were needed last month:
- Winter clothes
- Heating system maintenance
- Holiday decorations
- Gift wrapping supplies',
     CURRENT_TIMESTAMP - INTERVAL '45' DAY,
     CURRENT_TIMESTAMP - INTERVAL '60' DAY,
     CURRENT_TIMESTAMP - INTERVAL '30' DAY,
     TRUE,
     CURRENT_TIMESTAMP - INTERVAL '30' DAY,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Quick Reminder',
     'Don''t forget to call mom this weekend!',
     CURRENT_TIMESTAMP + INTERVAL '3' DAY,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser')),

    ('Learning Goals',
     'Skills to develop this year:
- Advanced Kotlin features
- Spring Boot best practices
- Database optimization
- Frontend frameworks (React/Vue)
- DevOps and CI/CD
- System design principles',
     CURRENT_TIMESTAMP + INTERVAL '30' DAY,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     FALSE,
     NULL,
     (SELECT id FROM users WHERE username = 'testuser'));
