from django.db import models

class User(models.Model):
	user_name = models.CharField(max_length=40)
	password = models.CharField(max_length=40)

	def __str__(self):
		return self.user_name

class Trip(models.Model):
	user_fkey = models.ForeignKey(User, on_delete=models.CASCADE)

	name = models.CharField(max_length=60)
	bike_used = models.CharField(max_length=40)
	phone_placement = models.CharField(max_length=40)

	is_public = models.BooleanField()

	data_file = models.FileField(upload_to='documents')

	def __str__(self):
		return self.name
