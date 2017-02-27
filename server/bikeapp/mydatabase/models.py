from django.db import models

class User(models.Model):
	user_name = models.CharField(max_length=40)
	password = models.CharField(max_length=40)

	def __str__(self):
		return self.user_name

class Trip(models.Model):
	user_fkey = models.ForeignKey(User, on_delete=models.CASCADE)

	is_public = models.BooleanField()

	name = models.CharField(max_length=60)
	bike_used = models.CharField(max_length=40)
	phone_placement = models.CharField(max_length=40)

	trip_data = models.FileField(upload_to='tripData/')

	def __str__(self):
		return self.name

class Rating(models.Model):
	latitude = models.FloatField()
	longitude = models.FloatField()

	rating = models.FloatField()
	count = models.IntegerField()

	def __str__(self):
		return str(self.latitude) + ' - ' + str(self.longitude)