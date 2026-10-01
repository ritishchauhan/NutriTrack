import { neon } from "@neondatabase/serverless";

const dbUrl = "postgresql://neondb_owner:npg_OkfzxlcauY87@ep-sweet-frost-aztswc2h-pooler.c-3.ap-southeast-1.aws.neon.tech/neondb?channel_binding=require&sslmode=require";

async function setupTables() {
  const sql = neon(dbUrl);
  
  console.log("Checking existing tables...");
  const tables = await sql`
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public';
  `;
  console.log("Existing tables:", tables);

  console.log("Creating tracked_meals and user_profiles tables if not exists...");
  await sql`
    CREATE TABLE IF NOT EXISTS user_profiles (
      user_id VARCHAR(128) PRIMARY KEY,
      name VARCHAR(255),
      email VARCHAR(255),
      calorie_target INT DEFAULT 2000,
      protein_target INT DEFAULT 150,
      carbs_target INT DEFAULT 200,
      fat_target INT DEFAULT 65,
      fiber_target INT DEFAULT 25,
      water_target DOUBLE PRECISION DEFAULT 2.4,
      water_logged DOUBLE PRECISION DEFAULT 0.0,
      streak_days INT DEFAULT 0,
      weight_kg DOUBLE PRECISION DEFAULT 70.0,
      height_cm DOUBLE PRECISION DEFAULT 170.0,
      fitness_goal VARCHAR(64) DEFAULT 'LOSE_WEIGHT',
      dietary_preference VARCHAR(64) DEFAULT 'Non-veg',
      activity_level VARCHAR(64) DEFAULT 'Sedentary',
      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );
  `;

  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS fiber_target INT DEFAULT 25;`;
  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS water_logged DOUBLE PRECISION DEFAULT 0.0;`;
  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS streak_days INT DEFAULT 0;`;
  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS weight_kg DOUBLE PRECISION DEFAULT 70.0;`;
  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS height_cm DOUBLE PRECISION DEFAULT 170.0;`;
  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS fitness_goal VARCHAR(64) DEFAULT 'LOSE_WEIGHT';`;
  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS dietary_preference VARCHAR(64) DEFAULT 'Non-veg';`;
  await sql`ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS activity_level VARCHAR(64) DEFAULT 'Sedentary';`;

  await sql`
    CREATE TABLE IF NOT EXISTS tracked_meals (
      id VARCHAR(128) PRIMARY KEY,
      user_id VARCHAR(128) NOT NULL,
      food_name VARCHAR(255) NOT NULL,
      calories DOUBLE PRECISION NOT NULL DEFAULT 0,
      protein DOUBLE PRECISION NOT NULL DEFAULT 0,
      carbs DOUBLE PRECISION NOT NULL DEFAULT 0,
      fat DOUBLE PRECISION NOT NULL DEFAULT 0,
      fiber DOUBLE PRECISION DEFAULT 0,
      portion_multiplier DOUBLE PRECISION DEFAULT 1.0,
      meal_type VARCHAR(64) DEFAULT 'Breakfast',
      barcode VARCHAR(128),
      image_url TEXT,
      logged_at BIGINT NOT NULL,
      details TEXT,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );
  `;

  await sql`
    CREATE INDEX IF NOT EXISTS idx_tracked_meals_user_id ON tracked_meals(user_id);
  `;
  await sql`
    CREATE INDEX IF NOT EXISTS idx_tracked_meals_logged_at ON tracked_meals(logged_at);
  `;
  await sql`
    CREATE INDEX IF NOT EXISTS idx_tracked_meals_user_logged_desc ON tracked_meals(user_id, logged_at DESC);
  `;
  await sql`
    CREATE INDEX IF NOT EXISTS idx_tracked_meals_user_food ON tracked_meals(user_id, food_name);
  `;

  console.log("Tables and indexes verified/optimized successfully in Neon!");
}

setupTables().catch(err => {
  console.error("Migration error:", err);
  process.exit(1);
});
