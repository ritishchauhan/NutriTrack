
@file:Suppress(
  "KotlinRedundantDiagnosticSuppress",
  "PropertyName",
  "MayBeConstant",
  "RedundantVisibilityModifier",
  "RedundantCompanionReference",
  "RemoveEmptyClassBody",
  "SpellCheckingInspection",
  "unused",
)

package com.example.macro_tracker.dataconnect



public interface UpsertUserProfileMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      NutritrackConnectorConnector,
      UpsertUserProfileMutation.Data,
      UpsertUserProfileMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
    val name: com.google.firebase.dataconnect.OptionalVariable<String?>,
  
    val email: com.google.firebase.dataconnect.OptionalVariable<String?>,
  
    val calorieTarget: Int,
  
    val proteinTarget: Int,
  
    val carbsTarget: Int,
  
    val fatTarget: Int,
  
    val fiberTarget: Int,
  
    val waterTarget: Double,
  
    val waterLogged: Double,
  
    val streakDays: Int,
  
    val weightKg: Double,
  
    val heightCm: Double,
  
    val fitnessGoal: String,
  
    val dietaryPreference: com.google.firebase.dataconnect.OptionalVariable<String?>,
  
    val activityLevel: com.google.firebase.dataconnect.OptionalVariable<String?>,
  
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      
      @BuilderDsl
      public interface Builder {
        public var userId: String
        public var name: String?
        public var email: String?
        public var calorieTarget: Int
        public var proteinTarget: Int
        public var carbsTarget: Int
        public var fatTarget: Int
        public var fiberTarget: Int
        public var waterTarget: Double
        public var waterLogged: Double
        public var streakDays: Int
        public var weightKg: Double
        public var heightCm: Double
        public var fitnessGoal: String
        public var dietaryPreference: String?
        public var activityLevel: String?
        
      }

      public companion object {
        
        @Suppress("NAME_SHADOWING")
        public fun build(
          userId: String,calorieTarget: Int,proteinTarget: Int,carbsTarget: Int,fatTarget: Int,fiberTarget: Int,waterTarget: Double,waterLogged: Double,streakDays: Int,weightKg: Double,heightCm: Double,fitnessGoal: String,
          block_: Builder.() -> Unit
        ): Variables {
          var userId= userId
            var name: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var email: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var calorieTarget= calorieTarget
            var proteinTarget= proteinTarget
            var carbsTarget= carbsTarget
            var fatTarget= fatTarget
            var fiberTarget= fiberTarget
            var waterTarget= waterTarget
            var waterLogged= waterLogged
            var streakDays= streakDays
            var weightKg= weightKg
            var heightCm= heightCm
            var fitnessGoal= fitnessGoal
            var dietaryPreference: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var activityLevel: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var userId: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { userId = value_ }
              
            override var name: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { name = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var email: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { email = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var calorieTarget: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { calorieTarget = value_ }
              
            override var proteinTarget: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { proteinTarget = value_ }
              
            override var carbsTarget: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { carbsTarget = value_ }
              
            override var fatTarget: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { fatTarget = value_ }
              
            override var fiberTarget: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { fiberTarget = value_ }
              
            override var waterTarget: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { waterTarget = value_ }
              
            override var waterLogged: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { waterLogged = value_ }
              
            override var streakDays: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { streakDays = value_ }
              
            override var weightKg: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { weightKg = value_ }
              
            override var heightCm: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { heightCm = value_ }
              
            override var fitnessGoal: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { fitnessGoal = value_ }
              
            override var dietaryPreference: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { dietaryPreference = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var activityLevel: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { activityLevel = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              userId=userId,name=name,email=email,calorieTarget=calorieTarget,proteinTarget=proteinTarget,carbsTarget=carbsTarget,fatTarget=fatTarget,fiberTarget=fiberTarget,waterTarget=waterTarget,waterLogged=waterLogged,streakDays=streakDays,weightKg=weightKg,heightCm=heightCm,fitnessGoal=fitnessGoal,dietaryPreference=dietaryPreference,activityLevel=activityLevel,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val userProfile_upsert: UserProfileKey,
  
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpsertUserProfile"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpsertUserProfileMutation.ref(
  
    userId: String,calorieTarget: Int,proteinTarget: Int,carbsTarget: Int,fatTarget: Int,fiberTarget: Int,waterTarget: Double,waterLogged: Double,streakDays: Int,weightKg: Double,heightCm: Double,fitnessGoal: String,

  
    block_: UpsertUserProfileMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpsertUserProfileMutation.Data,
    UpsertUserProfileMutation.Variables
  > =
  ref(
    
      UpsertUserProfileMutation.Variables.build(
        userId=userId,calorieTarget=calorieTarget,proteinTarget=proteinTarget,carbsTarget=carbsTarget,fatTarget=fatTarget,fiberTarget=fiberTarget,waterTarget=waterTarget,waterLogged=waterLogged,streakDays=streakDays,weightKg=weightKg,heightCm=heightCm,fitnessGoal=fitnessGoal,
  
    block_
      )
    
  )

public suspend fun UpsertUserProfileMutation.execute(

  
    
      userId: String,calorieTarget: Int,proteinTarget: Int,carbsTarget: Int,fatTarget: Int,fiberTarget: Int,waterTarget: Double,waterLogged: Double,streakDays: Int,weightKg: Double,heightCm: Double,fitnessGoal: String,

  
    block_: UpsertUserProfileMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpsertUserProfileMutation.Data,
    UpsertUserProfileMutation.Variables
  > =
  ref(
    
      userId=userId,calorieTarget=calorieTarget,proteinTarget=proteinTarget,carbsTarget=carbsTarget,fatTarget=fatTarget,fiberTarget=fiberTarget,waterTarget=waterTarget,waterLogged=waterLogged,streakDays=streakDays,weightKg=weightKg,heightCm=heightCm,fitnessGoal=fitnessGoal,
  
    block_
    
  ).execute()


